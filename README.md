An assignment I wrote a long time ago for a class.
It's a jar file that injects its code into other jar files on the local machine (a locally contained virus).
Probably really outdated now, just uploading so I don't lose it.
GPL licensed solely because I don't want anybody copying my work for school credits.






#Compile virus and victim class
javac 'VirusAgain.java';
javac 'HelloWorld.java';

#Run the virus on the HelloWorld.class
java VirusAgain;

#Examine the output file (infected class)
cd out;
javap -v 'HelloWorld.class' | less;

#Run the newly infected class.
cd out;
java HelloWorld;

