# [X2] 护腿 key equipment.legs | 期望匹配
scoreboard objectives add tt dummy
clear @s
item replace entity @s armor.legs with minecraft:diamond_leggings 1
execute if items entity @s armor.legs minecraft:diamond_leggings run scoreboard players set #x2s tt 1
execute unless items entity @s armor.legs minecraft:diamond_leggings run scoreboard players set #x2s tt 0
execute if data entity @s {equipment:{legs:{id:"minecraft:diamond_leggings"}}} run scoreboard players set #x2 tt 1
execute unless data entity @s {equipment:{legs:{id:"minecraft:diamond_leggings"}}} run scoreboard players set #x2 tt 0
