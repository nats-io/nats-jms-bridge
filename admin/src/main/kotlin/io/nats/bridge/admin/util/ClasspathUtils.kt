package io.nats.bridge.admin.util

import java.io.File
import java.net.URI
import java.net.URL
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.spi.FileSystemProvider


object ClasspathUtils {


    fun paths(clazz: Class<*>, resource: String): List<Path> {

        var list = pathsFromClassLoader(Thread.currentThread().contextClassLoader, resource)
        if (list.isEmpty()) {
            list = pathsFromClassLoader(clazz.classLoader, resource)
        }
        if (list.isEmpty() && resource.startsWith("/")) {
            return paths(clazz,  resource.substring(1))
        }
        return list
    }


    private fun classpathResources(loader: ClassLoader, resource: String): List<URL> {

        val resources = loader.getResources(resource)
        val list: List<URL> = resources.toList()
        if (list.isEmpty() && resource.startsWith("/")) {
            return classpathResources(loader, resource.substring(1))
        }
        return list
    }



    /**
     * Load the listFromClassLoader
     * @param loader
     * @param resource
     * @return
     */
    private fun pathsFromClassLoader(loader: ClassLoader, resource: String): List<Path> {
        val resourceURLs = classpathResources(loader, resource)
        val resourcePaths: MutableList<Path> = mutableListOf()
        val pathToZipFileSystems: MutableMap<URI, FileSystem> = HashMap() //So you don't have to keep loading the same jar/zip file.
        for (resourceURL in resourceURLs) {
            if (resourceURL.protocol == "jar") {
                pathsFromJar(resourcePaths, resourceURL, pathToZipFileSystems)
            } else {
                pathsFromFileSystem(resourcePaths, resourceURL)
            }
        }
        return resourcePaths
    }


    private val isWindows = System.getProperty("os.name").contains("Windows")

    private fun createURI(path: String): URI {
        return if (!isWindows) {
            URI.create(path)
        } else {
            if (path.contains("\\") || path.startsWith("C:") || path.startsWith("D:")) {
                var newPath: String = File(path).toURI().toString()
                if (newPath.startsWith("file:/C:")) {
                    newPath = newPath.substring(8)
                    URI.create(newPath)
                } else {
                    URI.create(newPath)
                }
            } else {
                URI.create(path)
            }
        }
    }

    private fun uriToPath(uri: URI): Path {
        val thePath: Path?
        if (isWindows) {
            var newPath = uri.path
            if (newPath.startsWith("/C:")) {
                newPath = newPath.substring(3)
            }
            thePath = FileSystems.getDefault().getPath(newPath)
        } else {
            thePath = FileSystems.getDefault().getPath(uri.path)
        }
        return thePath
    }


    private fun pathsFromFileSystem(resourcePaths: MutableList<Path>, u: URL) {
        val fileURI: URI = createURI(u.toString())
        resourcePaths.add(uriToPath(fileURI))

    }

    private fun loadFileSystemProvider(providerType: String): FileSystemProvider {
        var provider: FileSystemProvider? = null
        for (p in FileSystemProvider.installedProviders()) {
            if (providerType == p.scheme) {
                provider = p
                break
            }
        }
        return provider!!
    }


    private fun zipFileSystem(fileJarURI: URI?): FileSystem {
        val env: Map<String, Any?> = mapOf("create" to "true" as Any)
        val provider: FileSystemProvider? = loadFileSystemProvider("jar")
        var fs: FileSystem? = null
        try {
            fs = provider?.getFileSystem(fileJarURI)
        } catch (ex: Exception) {
            if (provider != null) {
                fs = provider.newFileSystem(fileJarURI, env)
            }
        }
        return fs!!
    }



    private fun pathsFromJar(resourcePaths: MutableList<Path>, resourceURL: URL, pathToZipFileSystems: MutableMap<URI, FileSystem>) {
        val str = resourceURL.toString()
        val strings: List<String> = str.split('!')
        val fileJarURI = URI.create(strings[0])
        val resourcePath = strings[1]
        if (!pathToZipFileSystems.containsKey(fileJarURI)) {
            pathToZipFileSystems[fileJarURI] = zipFileSystem(fileJarURI)
        }
        val fileSystem = pathToZipFileSystems[fileJarURI]
        val path = fileSystem?.getPath(resourcePath)

        if (path != null) {
            resourcePaths.add(path)
        }

    }



}