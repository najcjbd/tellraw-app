# [V1] 无 count 字段 | 只看实测值
scoreboard objectives add tt dummy
clear @s
item replace entity @s hotbar.5 with minecraft:diamond 1
execute if items entity @s hotbar.5 minecraft:diamond run scoreboard players set #v1s tt 1
execute unless items entity @s hotbar.5 minecraft:diamond run scoreboard players set #v1s tt 0
execute if data entity @s {Inventory:[{Slot:5b,id:"minecraft:diamond"}]} run scoreboard players set #v1 tt 1
execute unless data entity @s {Inventory:[{Slot:5b,id:"minecraft:diamond"}]} run scoreboard players set #v1 tt 0
