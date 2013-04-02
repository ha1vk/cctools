#!/bin/sh

suffix=$1

zip -9 -ry --exclude=*.svn* tmp/cctools-examples$suffix.zip Examples
