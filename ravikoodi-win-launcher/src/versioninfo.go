// Auto-generated file by goversioninfo. Do not edit.
package main

import (
	"encoding/json"

	"github.com/josephspurrier/goversioninfo"
)

func unmarshalGoVersionInfo(b []byte) goversioninfo.VersionInfo {
	vi := goversioninfo.VersionInfo{}
	json.Unmarshal(b, &vi)
	return vi
}

var versionInfo = unmarshalGoVersionInfo([]byte(`{
	"FixedFileInfo":{
		"FileVersion": {
			"Major": 1,
			"Minor": 1,
			"Patch": 7,
			"Build": 0
		},
		"ProductVersion": {
			"Major": 1,
			"Minor": 1,
			"Patch": 7,
			"Build": 0
		},
		"FileFlagsMask": "3f",
		"FileFlags": "",
		"FileOS": "040004",
		"FileType": "01",
		"FileSubType": "00"
	},
	"StringFileInfo":{
		"Comments": "RaviKoodi media server.",
		"CompanyName": "Igor Maznitsa",
		"FileDescription": "RaviKoodi launcher",
		"FileVersion": "v1.1.7.0",
		"InternalName": "ravikoodi.exe",
		"LegalCopyright": "Copyright (c) 2018-2021 Igor Maznitsa",
		"LegalTrademarks": "",
		"OriginalFilename": "ravikoodi.exe",
		"PrivateBuild": "",
		"ProductName": "RaviKoodi launcher",
		"ProductVersion": "v1.1.7.0",
		"SpecialBuild": ""
	},
	"VarFileInfo":{
		"Translation": {
			"LangID": 1033,
			"CharsetID": 1200
		}
	}
}`))
