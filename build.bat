pushd message
call gradle clean publishMavenJavaPublicationToMavenLocal
call gradle depend > ..\dependencies-message.txt
popd

pushd core
call gradle clean publishMavenJavaPublicationToMavenLocal
call gradle depend > ..\dependencies-core.txt
popd

pushd admin
call gradle clean publishMavenJavaPublicationToMavenLocal
call gradle depend > ..\dependencies-admin.txt
popd

pushd examples
call gradle clean build
call gradle depend > ..\dependencies-examples.txt
popd

pushd example-transform
call gradle clean build
call gradle depend > ..\dependencies-transforms.txt
popd

pushd mavenBridge
call mvn clean package -Dbridge-snapshot=-SNAPSHOT
call mvn dependency:tree -Dbridge-snapshot=-SNAPSHOT > ..\dependencies-mavenBridge.txt
popd
