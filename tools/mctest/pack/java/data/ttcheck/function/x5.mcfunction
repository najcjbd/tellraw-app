# [X5] EnderItems 不带 Slot（只测有没有） | 期望匹配
scoreboard objectives add tt dummy
clear @s
item replace entity @s enderchest.0 with minecraft:diamond 1
execute if items entity @s enderchest.0 minecraft:diamond run scoreboard players set #x5s tt 1
execute unless items entity @s enderchest.0 minecraft:diamond run scoreboard players set #x5s tt 0
execute if data entity @s {EnderItems:[{id:"minecraft:diamond"}]} run scoreboard players set #x5 tt 1
execute unless data entity @s {EnderItems:[{id:"minecraft:diamond"}]} run scoreboard players set #x5 tt 0
