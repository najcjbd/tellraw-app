# [B8] inventory.0 是否 = 背包第0格 | 只看实测值
scoreboard objectives add tt dummy
clear @s
item replace entity @s inventory.0 with minecraft:diamond 1
execute if items entity @s inventory.0 minecraft:diamond run scoreboard players set #b8s tt 1
execute unless items entity @s inventory.0 minecraft:diamond run scoreboard players set #b8s tt 0
execute if items entity @s inventory.0 minecraft:diamond run scoreboard players set #b8 tt 1
execute unless items entity @s inventory.0 minecraft:diamond run scoreboard players set #b8 tt 0
