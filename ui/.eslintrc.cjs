module.exports = {
	"ignorePatterns": ["dist/**/*"],
	"env": {
		"browser": true,
		"es2021": true
	},
	"extends": [
		// "eslint:recommended",
		// "plugin:@typescript-eslint/recommended",
		"plugin:vue/vue3-essential"
	],
	"overrides": [
		{
			"env": {
				"node": true
			},
			"files": [
				".eslintrc.{js,cjs}"
			],
			"parserOptions": {
				"sourceType": "script"
			}
		}
	],
	"parserOptions": {
		"ecmaVersion": "latest",
		"parser": "@typescript-eslint/parser",
		"sourceType": "module"
	},
	"plugins": [
		"@typescript-eslint",
		"vue"
	],
	"rules": {
		"indent": [
			"error",
			"tab"
		],
		"vue/html-indent": [
			"error",
			"tab"
		],
		"vue/script-indent": [
			"error",
			"tab"
		],
		"linebreak-style": [
			"error",
			"unix"
		],
		"quotes": [
			"error",
			"double"
		],
		"semi": [
			"error",
			"always"
		],
		"vue/multi-word-component-names": "off"
	}
};
