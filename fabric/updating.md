# Update to new versions of Minecraft
- look for the following phrases via "Find in Files" (Ctrl + Shift + F):
  - dropResources
  - spawnAtLocation
  - destroyBlock
  - spawnAfterBreak
    - the player is given via the threadLocal in this case
    - if the method only calls one of the following, you can ignore it
      - tryDropExperience
  - check if every relevant result is already handled by the mixins