# [V3] 新式 count:1（整数） | 只看实测值
scoreboard objectives add tt dummy
clear @s
item replace entity @s hotbar.5 with minecraft:diamond 1
execute if items entity @s hotbar.5 minecraft:diamond run scoreboard players set #v3s tt 1
execute unless items entity @s hotbar.5 minecraft:diamond run scoreboard players set #v3s tt 0
execute if data entity @s {Inventory:[{Slot:5b,id:"minecraft:diamond",count:1}]} run scoreboard players set #v3 tt 1
execute unless data entity @s {Inventory:[{Slot:5b,id:"minecraft:diamond",count:1}]} run scoreboard players set #v3 tt 0
