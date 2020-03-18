import java.io.File

import scala.io.Source

object Markdown extends App {

  val rootDir = new File(".").getCanonicalFile

  val srcDir = rootDir//new File(rootDir)


  println(walkDir(srcDir))

  def getFileAsText(file: File, default: String=""): String = {
    if (file.exists() && !file.getName.contains("DS_Store")) {
      //println(file)
      Source.fromFile(file).getLines().mkString("\n ")
    } else
      default
  }

  def walkDir(dir:File): String = {

    val files = dir.listFiles().toList

    val stopWords = List("build/", ".gradle/", "gradle/", "gradlew", ".idea", ".jar", "markdown", ".git", ".bat", "README.md", "out.md")

    files.filter(f=>f.isFile).filter(f => !stopWords.exists(sw=>f.toString.contains(sw)))
      .map(f=>markDownForFile(f, getFileAsText(f))).mkString("\n") +
      files.filter(m=> m.isDirectory).map(d=>walkDir(d)).mkString("\n")
  }


  def markDownForFile(file:File, contents:String) : String = {

    val fileString = file.toString

    val index  = fileString.indexOf("nats-bridge/")

    val shortName = fileString.substring(index)

    val parts = shortName.split('.')

    if (parts.length < 2) {
      ""
    } else {
      val lang = parts.last

      s"""
#### $shortName
```$lang
$contents
```
   """.stripMargin
    }
  }

}