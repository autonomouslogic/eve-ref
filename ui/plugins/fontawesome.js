import {config, library} from "@fortawesome/fontawesome-svg-core";
import {FontAwesomeIcon} from "@fortawesome/vue-fontawesome";
import {faDiscord, faGithub, faPatreon, faYoutube} from "@fortawesome/free-brands-svg-icons";
import {faSquare as farSquare} from "@fortawesome/free-regular-svg-icons";
import {faSquare as fasSquare} from "@fortawesome/free-solid-svg-icons";

// This is important, we are going to let Nuxt worry about the CSS
config.autoAddCss = false;

// You can add your icons directly in this plugin. See other examples for how you
// can add other styles or just individual icons.
library.add(
	faPatreon,
	faGithub,
	faDiscord,
	faYoutube,
	farSquare,
	fasSquare
);

export default defineNuxtPlugin((nuxtApp) => {
	nuxtApp.vueApp.component("font-awesome-icon", FontAwesomeIcon, {});
});
