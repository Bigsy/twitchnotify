rm -rf ./release
mkdir release
zip -r ./release/twitchnotify.zip ./resources/release/* -x release/\* -x package.sh
