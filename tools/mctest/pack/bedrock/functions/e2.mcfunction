# [E2] 主手 | 期望匹配
scoreboard objectives add tt dummy
clear @s
replaceitem entity @s slot.weapon.mainhand 0 diamond_sword 1
execute if entity @s[hasitem={item=diamond_sword,location=slot.weapon.mainhand}] run scoreboard players set #e2s tt 1
execute unless entity @s[hasitem={item=diamond_sword,location=slot.weapon.mainhand}] run scoreboard players set #e2s tt 0
execute if entity @s[hasitem={item=diamond_sword,location=slot.weapon.mainhand}] run scoreboard players set #e2 tt 1
execute unless entity @s[hasitem={item=diamond_sword,location=slot.weapon.mainhand}] run scoreboard players set #e2 tt 0
