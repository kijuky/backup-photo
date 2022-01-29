import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import scala.collection.JavaConverters._
import scala.util.Try
import scala.util.matching.Regex
import scala.util.matching.Regex.Match

lazy val backupPhoto = inputKey[Unit]("backup photo")
backupPhoto := {
  val args: Seq[String] = complete.DefaultParsers.spaceDelimited("<arg>").parsed
  if (args.nonEmpty) {
    args.init.foreach { src =>
      val srcPath = Paths.get(src)
      val dstPath = Paths.get(args.last)

      // println(srcPath)
      // println(dstPath)

      Files
        .walk(srcPath, Int.MaxValue)
        .toList
        .asScala
        .groupBy { path =>
          // println(path)
          dateTime(path).map(_.format(DateTimeFormatter.ofPattern("yyyy-MM")))
        }
        .flatMap { case (optKey, value) =>
          optKey.map(_ -> value) // key = None を除く。
        }
        .foreach { case (yearMonth, paths) =>
          // println(yearMonth)
          paths.foreach { path =>
            Try {
              val src = path
              val dst = dstPath.resolve(yearMonth).resolve(path.getFileName)
              print(path)
              Files.move(src, dst, REPLACE_EXISTING)
              println(s" -> $dst")
            }.recover { case e: Exception =>
              println(s" throws $e")
            }
          }
        }
    }
  }
}

def dateTime(path: Path): Option[LocalDateTime] = {
  Try {
    // Exif 情報から撮影時刻を取得する。
    com.drew.imaging.ImageMetadataReader
      .readMetadata(path.toFile)
      .getDirectories
      .asScala
      .flatMap(_.getTags.asScala)
      .find(_.getTagName == "Date/Time Original")
      .map(_.getDescription)
      .map { d =>
        LocalDateTime
          .parse(d, DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss"))
      }
  }.toOption.flatten.orElse {
    // 例外が発生したり、Exif 情報がなかった場合は、ファイル名から撮影時刻を取得する。
    val fileName = path.getFileName.toString
    Stream[(Regex, Match => LocalDateTime)](
      raw"(\d{13})\.jpg".r
        -> { m =>
          LocalDateTime.ofInstant(
            Instant.ofEpochMilli(m.group(1).toLong),
            ZoneId.systemDefault()
          )
        },
      raw"dvr_(\d{4})(\d{2})(\d{2})_(\d{2})(\d{2})\.mp4".r
        -> { m =>
          LocalDateTime.of(
            m.group(1).toInt, // year
            m.group(2).toInt, // month
            m.group(3).toInt, // dayOfMonth
            m.group(4).toInt, // hour
            m.group(5).toInt // minute
          )
        },
      raw"(\d{4})-(\d{2})-(\d{2}) (\d{2})-(\d{2})-(\d{2})\.jpg".r
        -> { m =>
          LocalDateTime.of(
            m.group(1).toInt, // year
            m.group(2).toInt, // month
            m.group(3).toInt, // dayOfMonth
            m.group(4).toInt, // hour
            m.group(5).toInt, // minute
            m.group(6).toInt // second
          )
        },
      raw"Screenshot_(\d{4})(\d{2})(\d{2})-(\d{2})(\d{2})(\d{2})\.png".r
        -> { m =>
          LocalDateTime.of(
            m.group(1).toInt, // year
            m.group(2).toInt, // month
            m.group(3).toInt, // dayOfMonth
            m.group(4).toInt, // hour
            m.group(5).toInt, // minute
            m.group(6).toInt // second
          )
        },
      raw"VID_(\d{4})(\d{2})(\d{2})_(\d{2})(\d{2})(\d{2})\.mp4".r
        -> { m =>
          LocalDateTime.of(
            m.group(1).toInt, // year
            m.group(2).toInt, // month
            m.group(3).toInt, // dayOfMonth
            m.group(4).toInt, // hour
            m.group(5).toInt, // minute
            m.group(6).toInt // second
          )
        },
      raw"PXL_(\d{4})(\d{2})(\d{2})_(\d{2})(\d{2})(\d{2})(\d{3})\.mp4".r
        -> { m =>
          LocalDateTime.of(
            m.group(1).toInt, // year
            m.group(2).toInt, // month
            m.group(3).toInt, // dayOfMonth
            m.group(4).toInt, // hour
            m.group(5).toInt, // minute
            m.group(6).toInt, // second
            m.group(7).toInt * 1000000 // nanoOfSecond
          )
        },
      raw"(\d{4})(\d{2})(\d{2})_(\d{2})(\d{2})(\d{2})(\d{3})_iOS\.(MOV|mp4)".r
        -> { m =>
          LocalDateTime.of(
            m.group(1).toInt, // year
            m.group(2).toInt, // month
            m.group(3).toInt, // dayOfMonth
            m.group(4).toInt, // hour
            m.group(5).toInt, // minute
            m.group(6).toInt, // second
            m.group(7).toInt * 1000000 // nanoOfSecond
          )
        }
    ).flatMap { case (pattern, toLocalDateTime) =>
      pattern.findFirstMatchIn(fileName).map(toLocalDateTime)
    }.headOption
  }
}
