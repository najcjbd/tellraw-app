# [B12] weapon.offhand | 期望匹配
scoreboard objectives add tt dummy
clear @s
item replace entity @s weapon.offhand with minecraft:diamond_sword 1
execute if items entity @s weapon.offhand minecraft:diamond_sword run scoreboard players set #b12s tt 1
execute unless items entity @s weapon.offhand minecraft:diamond_sword run scoreboard players set #b12s tt 0
execute if items entity @s weapon.offhand minecraft:diamond_sword run scoreboard players set #b12 tt 1
execute unless items entity @s weapon.offhand minecraft:diamond_sword run scoreboard players set #b12 tt 0
