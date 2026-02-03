
fmt:
	sbt scalafmtAll

clean:
	-find -name target -exec rm -rf \{\} \;
	sbt clean
