# [Z7] 改名后的 enchantments 形状（**无 levels 外壳**，z7 实测纠正过） | 期望匹配
scoreboard objectives add tt dummy
clear @s
item replace entity @s weapon.mainhand with minecraft:diamond_sword[minecraft:enchantments={"minecraft:sharpness":3}] 1
execute if items entity @s weapon.mainhand minecraft:diamond_sword run scoreboard players set #z7s tt 1
execute unless items entity @s weapon.mainhand minecraft:diamond_sword run scoreboard players set #z7s tt 0
execute if items entity @s weapon.mainhand minecraft:diamond_sword[enchantments={"minecraft:sharpness":3}] run scoreboard players set #z7 tt 1
execute unless items entity @s weapon.mainhand minecraft:diamond_sword[enchantments={"minecraft:sharpness":3}] run scoreboard players set #z7 tt 0
