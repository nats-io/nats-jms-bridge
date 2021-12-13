pushd message
call gradle clean publishMavenJavaPublicationToMavenLocal
popd

pushd core
call gradle clean publishMavenJavaPublicationToMavenLocal
popd

pushd admin
call gradle clean publishMavenJavaPublicationToMavenLocal
popd
