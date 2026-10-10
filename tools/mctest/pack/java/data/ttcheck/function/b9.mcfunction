# [B9] container.9 是否 = 背包第0格 | 只看实测值
scoreboard objectives add tt dummy
clear @s
item replace entity @s inventory.0 with minecraft:diamond 1
execute if items entity @s inventory.0 minecraft:diamond run scoreboard players set #b9s tt 1
execute unless items entity @s inventory.0 minecraft:diamond run scoreboard players set #b9s tt 0
execute if items entity @s container.9 minecraft:diamond run scoreboard players set #b9 tt 1
execute unless items entity @s container.9 minecraft:diamond run scoreboard players set #b9 tt 0
