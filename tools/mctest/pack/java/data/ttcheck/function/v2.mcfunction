# [V2] 老式 count:1b | 只看实测值
scoreboard objectives add tt dummy
clear @s
item replace entity @s hotbar.5 with minecraft:diamond 1
execute if items entity @s hotbar.5 minecraft:diamond run scoreboard players set #v2s tt 1
execute unless items entity @s hotbar.5 minecraft:diamond run scoreboard players set #v2s tt 0
execute if data entity @s {Inventory:[{Slot:5b,id:"minecraft:diamond",count:1b}]} run scoreboard players set #v2 tt 1
execute unless data entity @s {Inventory:[{Slot:5b,id:"minecraft:diamond",count:1b}]} run scoreboard players set #v2 tt 0
