# backup-photo

写真や動画を、対象のフォルダの `yyyy-MM` フォルダにコピーします。写真は Exif 情報から撮影時刻（Date/Time Original）を取得します。Exif 情報から取得できなかった場合は、ファイル名から撮影時刻を推測します。

## 使い方

```shell
sbt "backupPhoto srcPath1 srcPath2 ... dstPath"
```
- srcPathN: コピー元のディレクトリ。サブディレクトリにある写真も対象。
- dstPath: コピー先のディレクトリ。
