# script to git pull and run MP2 code
import sys
import os
# os.system("git pull")
os.system("javac mypack/*.java")
os.system("javac *.java")
runner = "java Node "
runner += sys.argv[1]
os.system(runner)
