# [C1] Inventory 不含空/含钻石 | 期望匹配
scoreboard objectives add tt dummy
clear @s
item replace entity @s hotbar.0 with minecraft:diamond 1
execute if items entity @s hotbar.0 minecraft:diamond run scoreboard players set #c1s tt 1
execute unless items entity @s hotbar.0 minecraft:diamond run scoreboard players set #c1s tt 0
execute if data entity @s {Inventory:[{id:"minecraft:diamond"}]} run scoreboard players set #c1 tt 1
execute unless data entity @s {Inventory:[{id:"minecraft:diamond"}]} run scoreboard players set #c1 tt 0
