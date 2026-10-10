# [X6] 老式 tag:{…} 在 26.2 还能不能匹配（空 tag 应当匹配） | 只看实测值
scoreboard objectives add tt dummy
clear @s
item replace entity @s hotbar.0 with minecraft:diamond 1
execute if items entity @s hotbar.0 minecraft:diamond run scoreboard players set #x6s tt 1
execute unless items entity @s hotbar.0 minecraft:diamond run scoreboard players set #x6s tt 0
execute if data entity @s {Inventory:[{Slot:0b,id:"minecraft:diamond",tag:{}}]} run scoreboard players set #x6 tt 1
execute unless data entity @s {Inventory:[{Slot:0b,id:"minecraft:diamond",tag:{}}]} run scoreboard players set #x6 tt 0
