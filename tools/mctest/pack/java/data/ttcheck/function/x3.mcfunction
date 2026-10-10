# [X3] 靴子 key equipment.feet | 期望匹配
scoreboard objectives add tt dummy
clear @s
item replace entity @s armor.feet with minecraft:diamond_boots 1
execute if items entity @s armor.feet minecraft:diamond_boots run scoreboard players set #x3s tt 1
execute unless items entity @s armor.feet minecraft:diamond_boots run scoreboard players set #x3s tt 0
execute if data entity @s {equipment:{feet:{id:"minecraft:diamond_boots"}}} run scoreboard players set #x3 tt 1
execute unless data entity @s {equipment:{feet:{id:"minecraft:diamond_boots"}}} run scoreboard players set #x3 tt 0
