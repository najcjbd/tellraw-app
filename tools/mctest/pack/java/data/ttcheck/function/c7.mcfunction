# [C7] Slot:0b = 快捷栏0 | 期望匹配
scoreboard objectives add tt dummy
clear @s
item replace entity @s hotbar.0 with minecraft:diamond 1
execute if items entity @s hotbar.0 minecraft:diamond run scoreboard players set #c7s tt 1
execute unless items entity @s hotbar.0 minecraft:diamond run scoreboard players set #c7s tt 0
execute if data entity @s {Inventory:[{Slot:0b,id:"minecraft:diamond"}]} run scoreboard players set #c7 tt 1
execute unless data entity @s {Inventory:[{Slot:0b,id:"minecraft:diamond"}]} run scoreboard players set #c7 tt 0
