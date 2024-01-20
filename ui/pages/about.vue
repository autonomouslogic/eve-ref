<script setup lang="ts">
import ExternalLink from "~/components/helpers/ExternalLink.vue";

useHead({
	title: "About"
});
</script>

<template>
	<h1>About EVE Ref</h1>
  <p>
    EVE Ref is a reference site for the game EVE Online by CCP.
    It provides easy access to the information present in the game, such as ships, item stats, industry, reprocessing, systems, agents, etc.
    EVE Ref is created by <ExternalLink url="https://kennethjorgensen.com">Kenn</ExternalLink>,
    aka <ExternalLink url="https://evewho.com/character/1452072530">Dato Tovikov</ExternalLink>.
    Join the <ExternalLink url="https://discord.gg/fZYPAxFyXG">Discord</ExternalLink> to get in touch and participate.
    Source code is available on <ExternalLink url="https://github.com/autonomouslogic/eve-ref/">Github</ExternalLink>.
  </p>

  <h2>Support EVE Ref</h2>
  <p>
    <ExternalLink url="https://patreon.com/everef">Become a patron</ExternalLink> to support EVE Ref financially.
    EVE Ref is a free service with no ads, but it does cost money to run and takes time to build and maintain.
    Any and all contributions are greatly appreciated.
  </p>
  <p>
    You can also buy PLEX or Omega from <ExternalLink url="https://store.markeedragon.com/affiliate.php?id=933">Markeedragon</ExternalLink>
    using the code "everef" to get 3% off and support the site.
  </p>
  <p>
    ISK donations are also welcome and can be made directly to
    <ExternalLink url="https://evewho.com/character/1452072530">Dato Tovikov</ExternalLink> or
    <ExternalLink url="https://evewho.com/character/2113778331">EVE Ref</ExternalLink> of
    <ExternalLink url="https://evewho.com/corporation/98547654">EVE Ref Holdings</ExternalLink> [EVREF]:
    <div class="flex justify-around">
      <img src="~/assets/everef-char.png" alt="EVE Ref Logo" />
      <img src="~/assets/everef-corp.png" alt="EVE Ref Logo" />
    </div>
  </p>

  <h2>Copyright</h2>
  <p>
    EVE Ref is the property of <ExternalLink url="https://autonomouslogic.com">Autonomous Logic</ExternalLink>. All rights reserved.
  </p>
  <p>
    EVE Online and the EVE logo are the registered trademarks of CCP hf. All rights are reserved worldwide.
    All other trademarks are the property of their respective owners. EVE Online, the EVE logo, EVE and all associated
    logos and designs are the intellectual property of CCP hf.
    All artwork, screenshots, characters, vehicles, storylines, world facts or other recognizable features of the
    intellectual property relating to these trademarks are likewise the intellectual property of CCP hf. CCP hf.
    has granted permission to EVE Ref to use EVE Online and all associated logos and designs for promotional and
    information purposes on its website but does not endorse, and is not in any way affiliated with, EVE Ref.
    CCP is in no way responsible for the content on or functioning of this website, nor can it be liable for any damage
    arising from the use of this website.
  </p>
</template>

<style scoped>
p {
  @apply my-4;
}
</style>
