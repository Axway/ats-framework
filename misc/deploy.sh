#!/bin/bash

# save the version that is about to be released
RELEASE_VERSION=`echo $(mvn -q -Dexec.executable=echo -Dexec.args='${'project.version'}' --non-recursive exec:exec) | awk '{split($0, a, "-"); print a[1]}'`

echo "BEGIN releasing of ATS Framework VERSION $RELEASE_VERSION"

mvn release:clean -DautoVersionSubmodules=true -Darguments="-DskipTests=true -Dgpg.skip=false"
mvn release:prepare -DautoVersionSubmodules=true -Darguments="-DskipTests=true -Dgpg.skip=false" 
mvn release:perform -DautoVersionSubmodules=true -Darguments="-DskipTests=true -Dgpg.skip=false"

# delete the previous file
# Note that it is expected that the file (ats.version) contains only version=some_version
rm -rf corelibrary/src/main/resources/ats.version
# push new version
echo version=$RELEASE_VERSION > corelibrary/src/main/resources/ats.version

COMMIT_MSG="Change ats.version to "$RELEASE_VERSION""
echo $COMMIT_MSG
git add corelibrary/src/main/resources/ats.version
git commit -m "$COMMIT_MSG"
git push

echo "Release of ATS Framework VERSION $RELEASE_VERSION successful"
