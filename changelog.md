## Paper

- do not use magnetic's break-chained block logic if the player is not eligible to use magnetic
- fixes bug with break-chained blocks where some items were simply not dropped if the first-broken block did not also
  drop it
- now also drop byproducts of break-chained blocks like Cactus - Cactus Plant, Kelp - Kelp Plant
- also handle `PlayerHarvestEvent`, which catches sweet berries and cave vines
- fix bug when glow berries were dropped to the ground when the holding cave vines were broken