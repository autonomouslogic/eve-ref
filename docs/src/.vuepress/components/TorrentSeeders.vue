<template>
  <span :title="title">{{ text }}</span>
</template>

<script>
  // Seeder counts for every Deep Archive torrent, fetched once and shared by every instance on the page.
  const SEEDERS_URL = 'https://api.everef.net/v1/deep-archive/seeders'
  let seedersPromise = null

  function fetchSeeders () {
    if (!seedersPromise) {
      seedersPromise = fetch(SEEDERS_URL).then(res => {
        if (!res.ok) {
          throw new Error(`Unexpected status ${res.status}`)
        }
        return res.json()
      })
      seedersPromise.catch(() => {
        // Allow a retry on the next page visit.
        seedersPromise = null
      })
    }
    return seedersPromise
  }

  // Health indicator for a torrent's seeder count.
  function seedersEmoji (seeders) {
    if (seeders <= 3) {
      return '🔴'
    }
    if (seeders <= 9) {
      return '🟡'
    }
    return '🟢'
  }

  export default {
    props: {
      hash: {
        type: String,
        required: true
      }
    },
    data () {
      return {
        seeders: null,
        scrapedAt: null,
        error: false
      }
    },
    computed: {
      text () {
        if (this.error) {
          return '?'
        }
        return this.seeders === null ? '…' : `${seedersEmoji(this.seeders)} ${this.seeders}`
      },
      title () {
        if (this.error) {
          return 'Seeder count unavailable'
        }
        return this.scrapedAt ? `As of ${new Date(this.scrapedAt).toLocaleString()}` : ''
      }
    },
    // Runs in the browser only, never during the static build.
    mounted () {
      fetchSeeders()
        .then(data => {
          const count = data.seeders[this.hash.toLowerCase()]
          if (count === undefined) {
            this.error = true
            return
          }
          this.seeders = count
          this.scrapedAt = data.scraped_at
        })
        .catch(() => {
          this.error = true
        })
    }
  }
</script>
