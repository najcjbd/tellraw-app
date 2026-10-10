# [Y4] 损耗3的剑：data=3 应命中 | 期望匹配
scoreboard objectives add tt dummy
clear @s
replaceitem entity @s slot.weapon.mainhand 0 diamond_sword 1 3
execute if entity @s[hasitem={item=diamond_sword,location=slot.weapon.mainhand,data=3}] run scoreboard players set #y4s tt 1
execute unless entity @s[hasitem={item=diamond_sword,location=slot.weapon.mainhand,data=3}] run scoreboard players set #y4s tt 0
execute if entity @s[hasitem={item=diamond_sword,location=slot.weapon.mainhand,data=3}] run scoreboard players set #y4 tt 1
execute unless entity @s[hasitem={item=diamond_sword,location=slot.weapon.mainhand,data=3}] run scoreboard players set #y4 tt 0
