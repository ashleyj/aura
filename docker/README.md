# Build docker container
$ cd docker
$ docker build -t aura-docker . --no-cache

# Run docker container
$ docker run -it aura-docker /bin/bash

# Checking Aura
$ git clone --recursive git://github.com/ashleyj/aura

# Build
$ cd aura
$ vm/build.sh
