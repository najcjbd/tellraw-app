# [X4] 末影箱 nbt key 是 EnderItems、Slot 用箱内编号 | 期望匹配
scoreboard objectives add tt dummy
clear @s
item replace entity @s enderchest.0 with minecraft:diamond 1
execute if items entity @s enderchest.0 minecraft:diamond run scoreboard players set #x4s tt 1
execute unless items entity @s enderchest.0 minecraft:diamond run scoreboard players set #x4s tt 0
execute if data entity @s {EnderItems:[{Slot:0b,id:"minecraft:diamond"}]} run scoreboard players set #x4 tt 1
execute unless data entity @s {EnderItems:[{Slot:0b,id:"minecraft:diamond"}]} run scoreboard players set #x4 tt 0
