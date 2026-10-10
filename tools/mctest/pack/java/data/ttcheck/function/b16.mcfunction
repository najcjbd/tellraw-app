# [B16] count~ 区间 | 期望匹配
scoreboard objectives add tt dummy
clear @s
item replace entity @s hotbar.5 with minecraft:diamond 3
execute if items entity @s hotbar.5 minecraft:diamond[count=3] run scoreboard players set #b16s tt 1
execute unless items entity @s hotbar.5 minecraft:diamond[count=3] run scoreboard players set #b16s tt 0
execute if items entity @s hotbar.5 minecraft:diamond[count~{min:2,max:5}] run scoreboard players set #b16 tt 1
execute unless items entity @s hotbar.5 minecraft:diamond[count~{min:2,max:5}] run scoreboard players set #b16 tt 0
