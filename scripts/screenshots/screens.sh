# The screens of the store set, shared by android.sh and ios.sh: <file name>|<what to open>.
# "what to open" is a section name (home, explore, library, downloads, requests, ...)
# or title:<kind>:<tmdb id> (see ScreenshotLaunch in both apps).
SCREENS=(
  "01-home|home"
  "02-explore|explore"
  "03-title|title:movie:910004"
  "04-library|library"
  "05-downloads|downloads"
  "06-requests|requests"
)

DEMO_USER="noor"
DEMO_PASSWORD="demo"
DEMO_PORT="${DEMO_PORT:-6263}"
