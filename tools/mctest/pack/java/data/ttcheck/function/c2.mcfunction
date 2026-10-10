# [C2] 只在副手 -> Inventory 不应命中 | 期望不匹配
scoreboard objectives add tt dummy
clear @s
item replace entity @s weapon.offhand with minecraft:diamond 1
execute if items entity @s weapon.offhand minecraft:diamond run scoreboard players set #c2s tt 1
execute unless items entity @s weapon.offhand minecraft:diamond run scoreboard players set #c2s tt 0
execute if data entity @s {Inventory:[{id:"minecraft:diamond"}]} run scoreboard players set #c2 tt 1
execute unless data entity @s {Inventory:[{id:"minecraft:diamond"}]} run scoreboard players set #c2 tt 0
