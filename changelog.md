## Both loaders

- add options to get drops from fluids when placing a bucket of a fluid - [**#76**](https://github.com/btwonion/magnetic/issues/76)
    - the bucket works without an enchantment when enabled
    - options:
        - `buckets.enabled` Sets whether fluid drops caused by placing a bucket will be respected.
        - `buckets.abilityTimeout` Sets the amount of time in milliseconds that will have to pass before the effect will
          be disabled again. -1 will let the ability work forever.

## Fabric
- fix bug when brushing an Armadillo will throw an exception - [**#81**](https://github.com/btwonion/magnetic/issues/81)