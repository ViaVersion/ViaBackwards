#!/usr/bin/env bash

cd .. &&
echo "Start maven build" &&
mvn clean install &&
echo "Copying" &&
cd ./all/ &&
cp ./target/viabackwards-all-2.0-DEV.jar /home/adr/mc/Spigot/TestServers/BungeeCord/plugins/ &&
echo "Done."