
fmt:
	sbt scalafmtAll

clean:
	-find -name target -exec rm -rf \{\} \;
	sbt clean

build:
	sbt compile

dist: clean build
	publish-sbt-sonatype publishSigned

upload: dist
	publish-sbt-sonatype sonaUpload
