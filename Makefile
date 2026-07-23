.PHONY: compile clean

# Put all .class files under target/ (no Maven/Gradle needed)
compile:
	mkdir -p target
	find . -name '*.java' ! -path './target/*' -print0 | xargs -0 javac -d target

clean:
	rm -rf target
	find . -name '*.class' -delete
