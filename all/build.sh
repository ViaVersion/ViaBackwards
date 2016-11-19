#!/usr/bin/env bash

cd .. &&
echo "Start maven build" &&
mvn clean install &&
echo "Copying" &&
cd ./all/ &&
cp ./target/viabackwards-all-2.0.jar /home/adr/mc/Spigot/TestServers/BungeeCord/plugins/ &&
cp ./target/viabackwards-all-2.0.jar /home/adr/mc/Spigot/TestServers/1.11/plugins/ &&
echo "Done."