#!/usr/bin/env sh
set -eu
# Use matc from the official Filament v1.56.0 distribution to match the runtime.
# The checked-in .filamat assets mean ordinary Gradle builds do not need this tool.
cemetery_matc="${MATC:-matc}"
"$cemetery_matc" -p mobile -a opengl -a vulkan -S -o app/src/main/assets/materials/garden.filamat materials/garden.mat
"$cemetery_matc" -p mobile -a opengl -a vulkan -S -o app/src/main/assets/materials/outline.filamat materials/outline.mat
