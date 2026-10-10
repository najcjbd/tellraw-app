# [B10] armor.head 单格 | 期望匹配
scoreboard objectives add tt dummy
clear @s
item replace entity @s armor.head with minecraft:diamond_helmet 1
execute if items entity @s armor.head minecraft:diamond_helmet run scoreboard players set #b10s tt 1
execute unless items entity @s armor.head minecraft:diamond_helmet run scoreboard players set #b10s tt 0
execute if items entity @s armor.head minecraft:diamond_helmet run scoreboard players set #b10 tt 1
execute unless items entity @s armor.head minecraft:diamond_helmet run scoreboard players set #b10 tt 0
