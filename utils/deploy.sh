#!/bin/bash

# save the version number that is about to be commited
FUTURE_VERSION=""
while read LINE
do 
	if [[ "$LINE" == *"<version>"* ]]; then
		#echo "$LINE" # prints <version>the_version-SNAPSHOT</version>
		FUTURE_VERSION=`echo $LINE | awk '{split($0,a,"-"); print a[1]}'` # returns <version>the_version
		FUTURE_VERSION=`echo $FUTURE_VERSION | awk '{split($0,a,">"); print a[2]}'` # returns the_version
		break
	fi 
done < pom.xml

mvn release:clean -DautoVersionSubmodules=true -Darguments="-DskipTests=true -Dgpg.skip=false"
mvn release:prepare -DautoVersionSubmodules=true -Darguments="-DskipTests=true -Dgpg.skip=false" 
mvn release:perform -DautoVersionSubmodules=true -Darguments="-DskipTests=true -Dgpg.skip=false"

# delete the previous file
# Note that it is expected that the file (ats.version) contains only version=some_version
rm -rf corelibrary/src/main/resources/ats.version
echo version=$FUTURE_VERSION > corelibrary/src/main/resources/ats.version
# push new version

COMMIT_MSG="Change ats.version to "$FUTURE_VERSION""
git add corelibrary/src/main/resources/ats.version
git commit -m "$COMMIT_MSG"
git push
