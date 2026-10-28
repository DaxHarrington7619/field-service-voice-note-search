#!/usr/bin/env sh
set -eu

curl --fail-with-body --request POST http://localhost:8080/api/field-notes \
  --header 'Content-Type: application/json' \
  --data '{"noteId":"note-104","workOrderId":"WO-2048","transcript":"Replaced the relay. I will come back Tuesday to verify the temperature.","photoIds":["photo-before","photo-after"],"dispatchStatus":"COMPLETED","technicianFollowUp":false}'

curl --fail-with-body --get http://localhost:8080/api/field-notes/search \
  --data-urlencode 'q=Which completed repair needs a temperature check?' \
  --data-urlencode 'dispatchStatus=COMPLETED'
