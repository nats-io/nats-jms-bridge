package io.nats.bridge.admin.util

import java.io.BufferedReader
import java.io.File
import java.net.URI
import java.net.URL
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.spi.FileSystemProvider


object Classpaths {
    fun classpathResources(loader: ClassLoader, resource: String): List<URL> {
        var resource = resource
        val resources = loader.getResources(resource)
        val list: List<URL> = resources.toList()
        if (list.isEmpty() && resource.startsWith("/")) {
            resource = resource.substring(1)
            return classpathResources(loader, resource)
        }
        return list
    }

    fun classpathResources(clazz: Class<*>, resource: String): List<URL> {
        var resource = resource
        var list = classpathResources(Thread.currentThread().contextClassLoader, resource)
        if (list.isEmpty()) {
            list = classpathResources(clazz.classLoader, resource)
        }
        if (list.isEmpty() && resource.startsWith("/")) {
            resource = resource.substring(1)
            return classpathResources(clazz, resource)
        }
        return list
    }

    fun resources(clazz: Class<*>, resource: String): List<String> {
        var resource = resource
        var list = listFromClassLoader(Thread.currentThread().contextClassLoader, resource)
        if (list.isEmpty()) {
            list = listFromClassLoader(clazz.classLoader, resource)
        }
        if (list.isEmpty() && resource.startsWith("/")) {
            resource = resource.substring(1)
            return resources(clazz, resource)
        }
        return list
    }

    fun paths(clazz: Class<*>, resource: String): List<Path> {
        var resource = resource
        var list = pathsFromClassLoader(Thread.currentThread().contextClassLoader, resource)
        if (list.isEmpty()) {
            list = pathsFromClassLoader(clazz.classLoader, resource)
        }
        if (list.isEmpty() && resource.startsWith("/")) {
            resource = resource.substring(1)
            return paths(clazz, resource)
        }
        return list
    }

    /**
     * Load the listFromClassLoader
     * @param loader
     * @param resource
     * @return
     */
    fun listFromClassLoader(loader: ClassLoader, resource: String): List<String> {
        val resourceURLs = classpathResources(loader, resource)
        val resourcePaths: MutableList<String> = mutableListOf()
        val pathToZipFileSystems: MutableMap<URI, FileSystem> = HashMap() //So you don't have to keep loading the same jar/zip file.
        for (resourceURL in resourceURLs) {
            if (resourceURL.protocol == "jar") {
                resourcesFromJar(resourcePaths, resourceURL, pathToZipFileSystems)
            } else {
                resourcesFromFileSystem(resourcePaths, resourceURL)
            }
        }
        return resourcePaths
    }

    /**
     * Load the listFromClassLoader
     * @param loader
     * @param resource
     * @return
     */
    fun pathsFromClassLoader(loader: ClassLoader, resource: String): List<Path> {
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

    fun createURI(path: String): URI {
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

    fun uriToPath(uri: URI): Path {
        var thePath: Path? = null
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

    private fun resourcesFromFileSystem(resourcePaths: MutableList<String>, u: URL) {
        val fileURI: URI = createURI(u.toString())
        resourcePaths.add(uriToPath(fileURI).toString())

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
        val provider: FileSystemProvider = loadFileSystemProvider("jar")
        var fs: FileSystem? = null
        try {
            fs = provider.getFileSystem(fileJarURI)
        } catch (ex: Exception) {
            if (provider != null) {
                fs = provider.newFileSystem(fileJarURI, env)
            }
        }
        return fs!!
    }

    private fun resourcesFromJar(resourcePaths: MutableList<String>, resourceURL: URL, pathToZipFileSystems: MutableMap<URI, FileSystem>) {
        val str = resourceURL.toString()
        val strings: List<String> = str.split("!")
        val fileJarURI = URI.create(strings[0])
        val resourcePath = strings[1]
        if (!pathToZipFileSystems.containsKey(fileJarURI)) {
            pathToZipFileSystems[fileJarURI] = zipFileSystem(fileJarURI)
        }
        val fileSystem = pathToZipFileSystems[fileJarURI]
        val path = fileSystem!!.getPath(resourcePath)
        if (path != null) {
            resourcePaths.add(str)
        }
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

    private fun resourcePathsFromJar(resourcePaths: MutableList<Path>, resourceURL: URL, pathToZipFileSystems: MutableMap<URI, FileSystem>) {
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

    fun readPath(path: Path): String? {
        return read(Files.newBufferedReader(path, StandardCharsets.UTF_8))
    }

    private fun read(newBufferedReader: BufferedReader): String {
        return newBufferedReader.readText()
    }
}