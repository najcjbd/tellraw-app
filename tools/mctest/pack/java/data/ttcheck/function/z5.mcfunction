# [Z5] enderchest.0 是否存在 | 只看实测值
scoreboard objectives add tt dummy
clear @s
item replace entity @s enderchest.0 with minecraft:diamond 1
execute if items entity @s enderchest.0 minecraft:diamond run scoreboard players set #z5s tt 1
execute unless items entity @s enderchest.0 minecraft:diamond run scoreboard players set #z5s tt 0
execute if items entity @s enderchest.0 minecraft:diamond run scoreboard players set #z5 tt 1
execute unless items entity @s enderchest.0 minecraft:diamond run scoreboard players set #z5 tt 0
