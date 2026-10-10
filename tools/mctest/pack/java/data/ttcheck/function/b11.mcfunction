# [B11] weapon.mainhand | 期望匹配
scoreboard objectives add tt dummy
clear @s
item replace entity @s weapon.mainhand with minecraft:diamond_sword 1
execute if items entity @s weapon.mainhand minecraft:diamond_sword run scoreboard players set #b11s tt 1
execute unless items entity @s weapon.mainhand minecraft:diamond_sword run scoreboard players set #b11s tt 0
execute if items entity @s weapon.mainhand minecraft:diamond_sword run scoreboard players set #b11 tt 1
execute unless items entity @s weapon.mainhand minecraft:diamond_sword run scoreboard players set #b11 tt 0
