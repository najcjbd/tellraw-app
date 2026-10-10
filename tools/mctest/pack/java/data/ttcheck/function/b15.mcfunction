# [B15] count=1 不命中3个 | 期望不匹配
scoreboard objectives add tt dummy
clear @s
item replace entity @s hotbar.5 with minecraft:diamond 3
execute if items entity @s hotbar.5 minecraft:diamond[count=3] run scoreboard players set #b15s tt 1
execute unless items entity @s hotbar.5 minecraft:diamond[count=3] run scoreboard players set #b15s tt 0
execute if items entity @s hotbar.5 minecraft:diamond[count=1] run scoreboard players set #b15 tt 1
execute unless items entity @s hotbar.5 minecraft:diamond[count=1] run scoreboard players set #b15 tt 0
