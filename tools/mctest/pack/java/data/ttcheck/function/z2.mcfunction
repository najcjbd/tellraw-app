# [Z2] data target=* 是否合法 | 只看实测值
scoreboard objectives add tt dummy
clear @s
item replace entity @s inventory.0 with minecraft:diamond 1
execute if items entity @s inventory.0 minecraft:diamond run scoreboard players set #z2s tt 1
execute unless items entity @s inventory.0 minecraft:diamond run scoreboard players set #z2s tt 0
execute if data entity * {Inventory:[{id:"minecraft:diamond"}]} run scoreboard players set #z2 tt 1
execute unless data entity * {Inventory:[{id:"minecraft:diamond"}]} run scoreboard players set #z2 tt 0
